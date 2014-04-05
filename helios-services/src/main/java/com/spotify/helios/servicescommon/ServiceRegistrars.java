package com.spotify.helios.servicescommon;

import com.spotify.helios.serviceregistration.NopServiceRegistrar;
import com.spotify.helios.serviceregistration.NopServiceRegistrarFactory;
import com.spotify.helios.serviceregistration.ServiceRegistrar;
import com.spotify.helios.serviceregistration.ServiceRegistrarFactory;
import com.spotify.helios.serviceregistration.ServiceRegistrarLoader;
import com.spotify.helios.serviceregistration.ServiceRegistrarLoadingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class ServiceRegistrars {

  private static final Logger log = LoggerFactory.getLogger(ServiceRegistrars.class);

  /**
   * Create a registrar. Attempts to load it from a plugin if path is not null or using the app
   * class loader otherwise. If no registrar plugin was found, returns a nop registrar.
   */
  public static ServiceRegistrar createServiceRegistrar(final Path path,
                                                        final String address,
                                                        final String domain) {
    // Get a registrar factory
    final ServiceRegistrarFactory factory;
    if (path == null) {
      factory = createFactory();
    } else {
      factory = createFactory(path);
    }

    // Create the registrar
    if (address != null) {
      log.info("Creating service registrar with address: {}", address);
      return factory.create(address);
    } else if (domain != null) {
      log.info("Creating service registrar for domain: {}", domain);
      return domain.equals("localhost")
             ? factory.create("tcp://localhost:4999")
             : factory.createForDomain(domain);
    } else {
      log.info("No address nor domain configured, not creating service registrar.");
      return new NopServiceRegistrar();
    }
  }

  /**
   * Get a registrar factory from a plugin.
   */
  private static ServiceRegistrarFactory createFactory(final Path path) {
    final ServiceRegistrarFactory factory;
    final Path absolutePath = path.toAbsolutePath();
    try {
      factory = ServiceRegistrarLoader.load(absolutePath);
      final String name = factory.getClass().getName();
      log.info("Loaded service registrar plugin: {} ({})", name, absolutePath);
    } catch (ServiceRegistrarLoadingException e) {
      throw new RuntimeException("Unable to load service registrar plugin: " +
                                 absolutePath, e);
    }
    return factory;
  }

  /**
   * Get a registrar factory from the application class loader.
   */
  private static ServiceRegistrarFactory createFactory() {
    final ServiceRegistrarFactory factory;
    final ServiceRegistrarFactory installed;
    try {
      installed = ServiceRegistrarLoader.load();
    } catch (ServiceRegistrarLoadingException e) {
      throw new RuntimeException("Unable to load service registrar", e);
    }
    if (installed == null) {
      log.debug("No service registrar plugin configured");
      factory = new NopServiceRegistrarFactory();
    } else {
      factory = installed;
      final String name = factory.getClass().getName();
      log.info("Loaded installed service registrar: {}", name);
    }
    return factory;
  }
}