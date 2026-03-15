using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

namespace QuantumJwt;

/// <summary>
/// Extension methods for registering QuantumJwtClient in the ASP.NET Core DI container.
/// </summary>
public static class ServiceCollectionExtensions
{
    /// <summary>
    /// Adds the Quantum JWT client as a singleton service.
    /// </summary>
    /// <param name="services">The service collection.</param>
    /// <param name="configure">Action to configure options.</param>
    /// <returns>The service collection for chaining.</returns>
    public static IServiceCollection AddQuantumJwt(
        this IServiceCollection services,
        Action<QuantumJwtOptions> configure)
    {
        var options = new QuantumJwtOptions();
        configure(options);

        services.AddSingleton(options);
        services.AddSingleton<QuantumJwtClient>(sp =>
        {
            var logger = sp.GetService<ILogger<QuantumJwtClient>>();
            return new QuantumJwtClient(options, logger);
        });

        return services;
    }

    /// <summary>
    /// Adds the Quantum JWT client with default options.
    /// </summary>
    public static IServiceCollection AddQuantumJwt(this IServiceCollection services)
    {
        return services.AddQuantumJwt(_ => { });
    }
}
