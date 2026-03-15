using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.IdentityModel.Tokens;
using StackExchange.Redis;

namespace QuantumJwt;

/// <summary>
/// Configuration options for the Quantum JWT client.
/// </summary>
public class QuantumJwtOptions
{
    /// <summary>Redis connection string (default: "localhost:6379").</summary>
    public string RedisConnectionString { get; set; } = "localhost:6379";

    /// <summary>Token time-to-live (default: 15 minutes).</summary>
    public TimeSpan TokenTtl { get; set; } = TimeSpan.FromMinutes(15);

    /// <summary>Token issuer claim.</summary>
    public string Issuer { get; set; } = "quantum-auth";

    /// <summary>Enable IP binding validation (default: true).</summary>
    public bool IpBindingEnabled { get; set; } = true;

    /// <summary>HMAC secret for quantum watermarking (null = disabled).</summary>
    public string? WatermarkSecret { get; set; }
}

/// <summary>
/// Thrown when the Redis entropy pool is empty.
/// </summary>
public class EntropyExhaustedException : Exception
{
    public EntropyExhaustedException(string message) : base(message) { }
}

/// <summary>
/// Thrown when token validation fails.
/// </summary>
public class InvalidTokenException : Exception
{
    public InvalidTokenException(string message) : base(message) { }
}

/// <summary>
/// Thrown when a honey/poison token is detected.
/// </summary>
public class HoneyTokenException : Exception
{
    public HoneyTokenException(string message) : base(message) { }
}

/// <summary>
/// Main client for generating and validating Quantum JWT tokens.
/// Uses quantum-derived entropy from Redis to sign JWTs with unique per-token keys.
/// </summary>
public class QuantumJwtClient : IDisposable
{
    private const string EntropyPoolKey = "entropy:pool";
    private const string KeyPrefix = "key:";
    private const string PoisonKeysSet = "poison_keys";

    private readonly IConnectionMultiplexer _redis;
    private readonly QuantumJwtOptions _options;
    private readonly ILogger<QuantumJwtClient> _logger;
    private readonly bool _ownsConnection;

    /// <summary>
    /// Create a new client that manages its own Redis connection.
    /// </summary>
    public QuantumJwtClient(QuantumJwtOptions options, ILogger<QuantumJwtClient>? logger = null)
    {
        _options = options ?? throw new ArgumentNullException(nameof(options));
        _logger = logger ?? NullLogger<QuantumJwtClient>.Instance;
        _redis = ConnectionMultiplexer.Connect(options.RedisConnectionString);
        _ownsConnection = true;
    }

    /// <summary>
    /// Create a new client using an externally managed Redis connection.
    /// </summary>
    public QuantumJwtClient(
        IConnectionMultiplexer redis,
        QuantumJwtOptions options,
        ILogger<QuantumJwtClient>? logger = null)
    {
        _redis = redis ?? throw new ArgumentNullException(nameof(redis));
        _options = options ?? throw new ArgumentNullException(nameof(options));
        _logger = logger ?? NullLogger<QuantumJwtClient>.Instance;
        _ownsConnection = false;
    }

    /// <summary>
    /// Generate a signed JWT using a quantum-derived key from the entropy pool.
    /// </summary>
    /// <param name="subject">User identifier (sub claim).</param>
    /// <param name="additionalClaims">Extra claims to include.</param>
    /// <param name="clientIp">Client IP for IP binding (null to skip).</param>
    /// <returns>Signed JWT string.</returns>
    public async Task<string> GenerateTokenAsync(
        string subject,
        IDictionary<string, object>? additionalClaims = null,
        string? clientIp = null)
    {
        var db = _redis.GetDatabase();

        // 1. Pop a key from the entropy pool
        var keyB64 = await db.ListLeftPopAsync(EntropyPoolKey);
        if (keyB64.IsNull)
        {
            _logger.LogError("Entropy pool exhausted");
            throw new EntropyExhaustedException("No entropy available in pool");
        }

        // Decode URL-safe base64
        byte[] keyBytes = DecodeUrlSafeBase64(keyB64.ToString());

        // 2. Store key under a new kid with TTL
        string kid = Guid.NewGuid().ToString();
        string storedKeyB64 = EncodeUrlSafeBase64(keyBytes);
        await db.StringSetAsync($"{KeyPrefix}{kid}", storedKeyB64, _options.TokenTtl);
        _logger.LogDebug("Stored key with kid: {Kid}", kid);

        // 3. Build claims
        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, subject),
            new(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
        };

        if (_options.IpBindingEnabled && clientIp != null)
        {
            claims.Add(new Claim("ip", clientIp));
        }

        if (additionalClaims != null)
        {
            foreach (var kvp in additionalClaims)
            {
                claims.Add(new Claim(kvp.Key, kvp.Value?.ToString() ?? ""));
            }
        }

        // 4. Sign
        var securityKey = new SymmetricSecurityKey(keyBytes);
        var credentials = new SigningCredentials(securityKey, SecurityAlgorithms.HmacSha256);

        var now = DateTime.UtcNow;
        var tokenDescriptor = new SecurityTokenDescriptor
        {
            Subject = new ClaimsIdentity(claims),
            Issuer = _options.Issuer,
            IssuedAt = now,
            Expires = now.Add(_options.TokenTtl),
            SigningCredentials = credentials,
            AdditionalHeaderClaims = new Dictionary<string, object> { { "kid", kid } },
        };

        var handler = new JwtSecurityTokenHandler();
        string token = handler.CreateEncodedJwt(tokenDescriptor);
        return token;
    }

    /// <summary>
    /// Validate a JWT.
    /// </summary>
    /// <param name="token">The JWT string.</param>
    /// <param name="clientIp">Client IP for IP binding check (null to skip).</param>
    /// <returns>The validated claims principal.</returns>
    public async Task<ClaimsPrincipal> ValidateTokenAsync(string token, string? clientIp = null)
    {
        var db = _redis.GetDatabase();

        // 1. Extract kid from header without verification
        var handler = new JwtSecurityTokenHandler();
        JwtSecurityToken jwt;
        try
        {
            jwt = handler.ReadJwtToken(token);
        }
        catch (Exception ex)
        {
            throw new InvalidTokenException($"Cannot read token: {ex.Message}");
        }

        if (!jwt.Header.TryGetValue("kid", out var kidObj) || kidObj is not string kid)
        {
            throw new InvalidTokenException("Missing kid in token header");
        }

        // 2. Check honey token
        bool isPoison = await db.SetContainsAsync(PoisonKeysSet, kid);
        if (isPoison)
        {
            _logger.LogError("🍯 Honey token triggered! kid: {Kid}", kid);
            throw new HoneyTokenException("Honey token detected – possible breach");
        }

        // 3. Retrieve key from Redis
        var keyB64 = await db.StringGetAsync($"{KeyPrefix}{kid}");
        if (keyB64.IsNull)
        {
            throw new InvalidTokenException("Key expired or revoked");
        }

        byte[] keyBytes = DecodeUrlSafeBase64(keyB64.ToString());

        // 4. Validate signature and claims
        var securityKey = new SymmetricSecurityKey(keyBytes);
        var validationParams = new TokenValidationParameters
        {
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = securityKey,
            ValidateIssuer = true,
            ValidIssuer = _options.Issuer,
            ValidateAudience = false,
            ValidateLifetime = true,
            ClockSkew = TimeSpan.FromSeconds(30),
        };

        ClaimsPrincipal principal;
        try
        {
            principal = handler.ValidateToken(token, validationParams, out _);
        }
        catch (SecurityTokenException ex)
        {
            throw new InvalidTokenException($"Token validation failed: {ex.Message}");
        }

        // 5. IP binding check
        if (_options.IpBindingEnabled && clientIp != null)
        {
            var tokenIp = principal.FindFirst("ip")?.Value;
            if (tokenIp != null && tokenIp != clientIp)
            {
                throw new InvalidTokenException("IP address mismatch");
            }
        }

        return principal;
    }

    /// <summary>
    /// Get the current entropy pool size.
    /// </summary>
    public async Task<long> GetPoolSizeAsync()
    {
        var db = _redis.GetDatabase();
        return await db.ListLengthAsync(EntropyPoolKey);
    }

    public void Dispose()
    {
        if (_ownsConnection)
        {
            _redis.Dispose();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static byte[] DecodeUrlSafeBase64(string input)
    {
        string padded = input.Replace('-', '+').Replace('_', '/');
        switch (padded.Length % 4)
        {
            case 2: padded += "=="; break;
            case 3: padded += "="; break;
        }
        return Convert.FromBase64String(padded);
    }

    private static string EncodeUrlSafeBase64(byte[] input)
    {
        return Convert.ToBase64String(input)
            .Replace('+', '-')
            .Replace('/', '_')
            .TrimEnd('=');
    }
}
