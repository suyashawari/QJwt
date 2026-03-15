using System.Security.Cryptography;
using Moq;
using StackExchange.Redis;
using Xunit;

namespace QuantumJwt.Tests;

/// <summary>
/// Unit tests for QuantumJwtClient using a mocked IConnectionMultiplexer.
/// </summary>
public class ClientTests
{
    private static readonly QuantumJwtOptions DefaultOptions = new()
    {
        Issuer = "test-issuer",
        TokenTtl = TimeSpan.FromMinutes(15),
        IpBindingEnabled = false,
    };

    /// <summary>
    /// Creates a mock Redis database that responds to the expected commands.
    /// </summary>
    private static (Mock<IConnectionMultiplexer> mux, Mock<IDatabase> db) CreateMockRedis(
        byte[]? poolKey = null)
    {
        var dbMock = new Mock<IDatabase>();
        var muxMock = new Mock<IConnectionMultiplexer>();
        muxMock.Setup(m => m.GetDatabase(It.IsAny<int>(), It.IsAny<object>()))
               .Returns(dbMock.Object);

        if (poolKey != null)
        {
            string keyB64 = EncodeUrlSafe(poolKey);
            dbMock.Setup(d => d.ListLeftPopAsync(
                    It.Is<RedisKey>(k => k == "entropy:pool"),
                    It.IsAny<CommandFlags>()))
                .ReturnsAsync((RedisValue)keyB64);
        }

        return (muxMock, dbMock);
    }

    private static string EncodeUrlSafe(byte[] data)
    {
        return Convert.ToBase64String(data)
            .Replace('+', '-')
            .Replace('/', '_')
            .TrimEnd('=');
    }

    // ------------------------------------------------------------------
    // 1. Happy Path — generate token produces a valid JWT string
    // ------------------------------------------------------------------
    [Fact]
    public async Task GenerateToken_ReturnsValidJwt()
    {
        byte[] key = RandomNumberGenerator.GetBytes(32);
        var (mux, db) = CreateMockRedis(poolKey: key);

        // Mock storing the key
        db.Setup(d => d.StringSetAsync(
                It.IsAny<RedisKey>(),
                It.IsAny<RedisValue>(),
                It.IsAny<TimeSpan?>(),
                It.IsAny<bool>(),
                It.IsAny<When>(),
                It.IsAny<CommandFlags>()))
            .ReturnsAsync(true);

        using var client = new QuantumJwtClient(mux.Object, DefaultOptions);
        string token = await client.GenerateTokenAsync("user_123");

        Assert.NotNull(token);
        Assert.Contains(".", token);  // JWT has three dot-separated parts
        Assert.Equal(3, token.Split('.').Length);
    }

    // ------------------------------------------------------------------
    // 2. Entropy Exhaustion
    // ------------------------------------------------------------------
    [Fact]
    public async Task GenerateToken_ThrowsWhenPoolEmpty()
    {
        var (mux, db) = CreateMockRedis(poolKey: null);

        // Pool returns null
        db.Setup(d => d.ListLeftPopAsync(
                It.IsAny<RedisKey>(),
                It.IsAny<CommandFlags>()))
            .ReturnsAsync(RedisValue.Null);

        using var client = new QuantumJwtClient(mux.Object, DefaultOptions);
        await Assert.ThrowsAsync<EntropyExhaustedException>(
            () => client.GenerateTokenAsync("user_123"));
    }

    // ------------------------------------------------------------------
    // 3. Honey Token Detection
    // ------------------------------------------------------------------
    [Fact]
    public async Task ValidateToken_ThrowsOnHoneyToken()
    {
        var (mux, db) = CreateMockRedis();

        // Simulate: any kid is a poison key
        db.Setup(d => d.SetContainsAsync(
                It.Is<RedisKey>(k => k == "poison_keys"),
                It.IsAny<RedisValue>(),
                It.IsAny<CommandFlags>()))
            .ReturnsAsync(true);

        // We need a real JWT to extract kid from header.
        // Generate one first using a valid key.
        byte[] key = RandomNumberGenerator.GetBytes(32);
        string keyB64 = EncodeUrlSafe(key);

        var (mux2, db2) = CreateMockRedis(poolKey: key);
        db2.Setup(d => d.StringSetAsync(
                It.IsAny<RedisKey>(),
                It.IsAny<RedisValue>(),
                It.IsAny<TimeSpan?>(),
                It.IsAny<bool>(),
                It.IsAny<When>(),
                It.IsAny<CommandFlags>()))
            .ReturnsAsync(true);

        using var genClient = new QuantumJwtClient(mux2.Object, DefaultOptions);
        string token = await genClient.GenerateTokenAsync("user_honey");

        // Now validate with the poisoned mock
        using var valClient = new QuantumJwtClient(mux.Object, DefaultOptions);
        await Assert.ThrowsAsync<HoneyTokenException>(
            () => valClient.ValidateTokenAsync(token));
    }

    // ------------------------------------------------------------------
    // 4. Pool Size
    // ------------------------------------------------------------------
    [Fact]
    public async Task GetPoolSize_ReturnsCorrectCount()
    {
        var (mux, db) = CreateMockRedis();

        db.Setup(d => d.ListLengthAsync(
                It.Is<RedisKey>(k => k == "entropy:pool"),
                It.IsAny<CommandFlags>()))
            .ReturnsAsync(42);

        using var client = new QuantumJwtClient(mux.Object, DefaultOptions);
        long size = await client.GetPoolSizeAsync();
        Assert.Equal(42, size);
    }
}
