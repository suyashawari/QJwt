using QuantumJwt;

var builder = WebApplication.CreateBuilder(args);

// Register the Quantum JWT client via DI
builder.Services.AddQuantumJwt(options =>
{
    options.RedisConnectionString = builder.Configuration.GetValue<string>("Redis:ConnectionString")
                                     ?? "localhost:6379";
    options.Issuer = "example-app";
    options.TokenTtl = TimeSpan.FromMinutes(15);
    options.IpBindingEnabled = true;
});

var app = builder.Build();

// ----- Endpoints -----

// Generate a token
app.MapPost("/auth/token", async (HttpContext ctx, QuantumJwtClient jwt) =>
{
    var username = ctx.Request.Query["username"].ToString();
    if (string.IsNullOrEmpty(username))
        return Results.BadRequest("username query parameter is required");

    var clientIp = ctx.Connection.RemoteIpAddress?.ToString();

    try
    {
        var token = await jwt.GenerateTokenAsync(username, clientIp: clientIp);
        return Results.Ok(new { token });
    }
    catch (EntropyExhaustedException)
    {
        return Results.StatusCode(503); // Service Unavailable
    }
});

// Validate a token
app.MapGet("/auth/validate", async (HttpContext ctx, QuantumJwtClient jwt) =>
{
    var authHeader = ctx.Request.Headers.Authorization.ToString();
    if (!authHeader.StartsWith("Bearer "))
        return Results.Unauthorized();

    var token = authHeader["Bearer ".Length..];
    var clientIp = ctx.Connection.RemoteIpAddress?.ToString();

    try
    {
        var principal = await jwt.ValidateTokenAsync(token, clientIp);
        var sub = principal.FindFirst("sub")?.Value;
        return Results.Ok(new { subject = sub, message = "Token is valid" });
    }
    catch (HoneyTokenException)
    {
        return Results.StatusCode(403);
    }
    catch (InvalidTokenException ex)
    {
        return Results.Unauthorized();
    }
});

// Pool status
app.MapGet("/pool/size", async (QuantumJwtClient jwt) =>
{
    var size = await jwt.GetPoolSizeAsync();
    return Results.Ok(new { poolSize = size });
});

app.Run();
