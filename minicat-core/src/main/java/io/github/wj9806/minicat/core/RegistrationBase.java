package io.github.wj9806.minicat.core;

import javax.servlet.Registration;
import javax.servlet.annotation.WebInitParam;
import java.util.*;

public abstract class RegistrationBase implements Registration.Dynamic {

    protected final String name;
    protected final String className;
    protected boolean isAsyncSupported;
    protected Map<String, String> initParameters = new HashMap<>();

    public RegistrationBase(String name, String className) {
        this.name = name;
        this.className = className;
    }

    //Registration.Dynamic
    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        this.isAsyncSupported = isAsyncSupported;
    }

    //Registration

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("Init parameter name or value cannot be null");
        }
        if (initParameters.containsKey(name)) {
            return false;
        }
        initParameters.put(name, value);
        return true;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        Set<String> conflicts = new HashSet<>();
        for (Map.Entry<String, String> entry : initParameters.entrySet()) {
            if (!setInitParameter(entry.getKey(), entry.getValue())) {
                conflicts.add(entry.getKey());
            }
        }
        return conflicts;
    }

    @Override
    public Map<String, String> getInitParameters() {
        return Collections.unmodifiableMap(initParameters);
    }

    protected void handleWebInitParams(WebInitParam[] webInitParams) {
        if (webInitParams != null) {
            Map<String, String> map = new HashMap<>();
            for (WebInitParam webInitParam : webInitParams) {
                map.put(webInitParam.name(), webInitParam.value());
            }
            if (!map.isEmpty())
                setInitParameters(map);
        }
    }
}
