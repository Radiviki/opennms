package org.opennms.features.topology.app.internal.gwt.client.service.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opennms.features.topology.app.internal.gwt.client.service.Filter;
import org.opennms.features.topology.app.internal.gwt.client.service.Registration;
import org.opennms.features.topology.app.internal.gwt.client.service.RegistrationHook;
import org.opennms.features.topology.app.internal.gwt.client.service.RegistrationListener;
import org.opennms.features.topology.app.internal.gwt.client.service.ServiceRegistry;
import org.opennms.features.topology.app.internal.gwt.client.service.filter.FilterParser;

public class DefaultServiceRegistry implements ServiceRegistry {

    /**
     * AnyFilter
     *
     * @author brozow
     */
    public class AnyFilter implements Filter {

        public boolean match(Map<String, String> properties) {
            return true;
        }

    }

    /** Constant <code>INSTANCE</code> */
    public static final DefaultServiceRegistry INSTANCE = new DefaultServiceRegistry();
    
    private class ServiceRegistration implements Registration {

        private boolean m_unregistered = false;
        private Object m_provider;
        private Map<String, String> m_properties;
        private Class<?>[] m_serviceInterfaces;
        
        public ServiceRegistration(Object provider, Map<String, String> properties, Class<?>[] serviceInterfaces) {
            m_provider = provider;
            m_properties = properties;
            m_serviceInterfaces = serviceInterfaces;
        }
        

        public Map<String, String> getProperties() {
            return m_properties == null ? null : Collections.unmodifiableMap(m_properties);
        }

        public Class<?>[] getProvidedInterfaces() {
            return m_serviceInterfaces;
        }
        
        public <T> T getProvider(Class<T> serviceInterface) {

            if (serviceInterface == null) throw new NullPointerException("serviceInterface may not be null");

            for( Class<?> cl : m_serviceInterfaces ) {
                if ( serviceInterface.equals( cl ) ) {
                    return cast( m_provider, serviceInterface);
                }
            }
            
            throw new IllegalArgumentException("Provider not registered with interface " + serviceInterface);
        }
        
        public Object getProvider() {
            return m_provider;
        }

        public ServiceRegistry getRegistry() {
            return DefaultServiceRegistry.this;
        }

        public boolean isUnregistered() {
            return m_unregistered;
        }
        
        public void unregister() {
            m_unregistered = true;
            DefaultServiceRegistry.this.unregister(this);
            m_provider = null;
        }
        
    }
    
    private MultivaluedMap<Class<?>, ServiceRegistration> m_registrationMap = MultivaluedMapImpl.synchronizedMultivaluedMap();
    private MultivaluedMap<Class<?>, RegistrationListener<?>> m_listenerMap = MultivaluedMapImpl.synchronizedMultivaluedMap();
    private List<RegistrationHook> m_hooks = new ArrayList<RegistrationHook>();
    
    /** {@inheritDoc} */
    public <T> T findProvider(Class<T> serviceInterface) {
        return findProvider(serviceInterface, null);
    }
    
    /** {@inheritDoc} */
    public <T> T findProvider(Class<T> serviceInterface, String filter) {
        Collection<T> providers = findProviders(serviceInterface, filter);
        for(T provider : providers) {
            return provider;
        }
        return null;
    }
    
    /** {@inheritDoc} */
    public <T> Collection<T> findProviders(Class<T> serviceInterface) {
        return findProviders(serviceInterface, null);
    }
    
    @SuppressWarnings({ "unchecked" })
    public <T> T cast(Object o, Class<T> c) {
        return (T) o;
    }

    /** {@inheritDoc} */
    public <T> Collection<T> findProviders(Class<T> serviceInterface, String filter) {
        
        Filter f = filter == null ? new AnyFilter() : new FilterParser().parse(filter);

        Set<ServiceRegistration> registrations = getRegistrations(serviceInterface);
        Set<T> providers = new LinkedHashSet<T>(registrations.size());
        for(ServiceRegistration registration : registrations) {
            if (f.match(registration.getProperties())) {
                providers.add(registration.getProvider(serviceInterface));
            }
        }
        return providers;
    }

    /**
     * <p>register</p>
     *
     * @param serviceProvider a {@link java.lang.Object} object.
     * @param services a {@link java.lang.Class} object.
     * @return a {@link org.opennms.core.soa.Registration} object.
     */
    public Registration register(Object serviceProvider, Class<?>... services) {
        return register(serviceProvider, (Map<String, String>)null, services);
    }

    /**
     * <p>register</p>
     *
     * @param serviceProvider a {@link java.lang.Object} object.
     * @param properties a {@link java.util.Map} object.
     * @param services a {@link java.lang.Class} object.
     * @return a {@link org.opennms.core.soa.Registration} object.
     */
    public Registration register(Object serviceProvider, Map<String, String> properties, Class<?>... services) {
        
        ServiceRegistration registration = new ServiceRegistration(serviceProvider, properties, services);
        
        for(Class<?> serviceInterface : services) {
            m_registrationMap.add(serviceInterface, registration);
        }
        
        fireRegistrationAdded(registration);
        
        for(Class<?> serviceInterface : services) {
            fireProviderRegistered(serviceInterface, registration);
        }

        
        return registration;

    }
    
    private void fireRegistrationAdded(ServiceRegistration registration) {
        for(RegistrationHook hook : m_hooks) {
            hook.registrationAdded(registration);
        }
    }

    private void fireRegistrationRemoved(ServiceRegistration registration) {
        for(RegistrationHook hook : m_hooks) {
            hook.registrationRemoved(registration);
        }
    }
    private <T> Set<ServiceRegistration> getRegistrations(Class<T> serviceInterface) {
        Set<ServiceRegistration> copy = m_registrationMap.getCopy(serviceInterface);
        return (copy == null ? Collections.<ServiceRegistration>emptySet() : copy);
    }

    private void unregister(ServiceRegistration registration) {
        
        for(Class<?> serviceInterface : registration.getProvidedInterfaces()) {
            m_registrationMap.remove(serviceInterface, registration);
        }
        
        fireRegistrationRemoved(registration);
        
        for(Class<?> serviceInterface : registration.getProvidedInterfaces()) {
            fireProviderUnregistered(serviceInterface, registration);
        }

    }

    /** {@inheritDoc} */
    public <T> void addListener(Class<T> service,  RegistrationListener<T> listener) {
        m_listenerMap.add(service, listener);
    }

    /** {@inheritDoc} */
    public <T> void addListener(Class<T> service,  RegistrationListener<T> listener, boolean notifyForExistingProviders) {

        if (notifyForExistingProviders) {
            
            Set<ServiceRegistration> registrations = null;
            
            synchronized (m_registrationMap) {
                m_listenerMap.add(service, listener);
                registrations = getRegistrations(service);
            }
            
            for(ServiceRegistration registration : registrations) {
                listener.providerRegistered(registration, registration.getProvider(service));
            }
            
        } else {
            
            m_listenerMap.add(service, listener);
            
        }
    }

    /** {@inheritDoc} */
    public <T> void removeListener(Class<T> service, RegistrationListener<T> listener) {
        m_listenerMap.remove(service, listener);
    }
    
    private <T> void fireProviderRegistered(Class<T> serviceInterface, Registration registration) {
        Set<RegistrationListener<T>> listeners = getListeners(serviceInterface);
        
        for(RegistrationListener<T> listener : listeners) {
            listener.providerRegistered(registration, registration.getProvider(serviceInterface));
        }
    }
    
    private <T> void fireProviderUnregistered(Class<T> serviceInterface, Registration registration) {
        Set<RegistrationListener<T>> listeners = getListeners(serviceInterface);
        
        for(RegistrationListener<T> listener : listeners) {
            listener.providerUnregistered(registration, registration.getProvider(serviceInterface));
        }
        
    }
    
    @SuppressWarnings("unchecked")
    private <T> Set<RegistrationListener<T>> getListeners(Class<T> serviceInterface) {
        Set<RegistrationListener<?>> listeners = m_listenerMap.getCopy(serviceInterface);
        return (Set<RegistrationListener<T>>) (listeners == null ? Collections.<RegistrationListener<T>>emptySet() : listeners);
    }

    @Override
    public void addRegistrationHook(RegistrationHook hook, boolean notifyForExistingProviders) {
        if (notifyForExistingProviders) {
            
            Set<ServiceRegistration> registrations = null;
            
            synchronized (m_registrationMap) {
                m_hooks.add(hook);
                registrations = getAllRegistrations();
            }
            
            for(ServiceRegistration registration : registrations) {
                hook.registrationAdded(registration);
            }
            
        } else {
            m_hooks.add(hook);
        }
    }

    @Override
    public void removeRegistrationHook(RegistrationHook hook) {
        m_hooks.remove(hook);
    }
    
    private Set<ServiceRegistration> getAllRegistrations() {
        Set<ServiceRegistration> registrations = new LinkedHashSet<ServiceRegistration>();
        
        for(Set<ServiceRegistration> registrationSet: m_registrationMap.values()) {
            registrations.addAll(registrationSet);
        }
        
        return registrations;
        
    }


}
