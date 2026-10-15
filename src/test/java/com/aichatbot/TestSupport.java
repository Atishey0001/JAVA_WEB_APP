package com.aichatbot;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny hand-made fakes for the servlet API (java.lang.reflect.Proxy), so filters can be tested
 * without a web server or a mocking library.
 */
public final class TestSupport {

    private TestSupport() {
    }

    /** Records what a filter did with the request/response. */
    public static class Recorder {
        public final Map<String, Object> sessionAttributes = new HashMap<>();
        public final Map<String, String> parameters = new HashMap<>();
        public boolean hasSession = true;
        public String method = "GET";
        public boolean chainCalled;
        public String redirectedTo;
        public String forwardedTo;
        public int status = 200;
        public final List<String> log = new ArrayList<>();

        public HttpServletRequest request() {
            HttpSession session = proxy(HttpSession.class, (name, args) -> switch (name) {
                case "getAttribute" -> sessionAttributes.get((String) args[0]);
                case "setAttribute" -> { sessionAttributes.put((String) args[0], args[1]); yield null; }
                default -> null;
            });
            return proxy(HttpServletRequest.class, (name, args) -> switch (name) {
                case "getSession" -> {
                    boolean create = args == null || args.length == 0 || (Boolean) args[0];
                    yield (hasSession || create) ? session : null;
                }
                case "getMethod" -> method;
                case "getContextPath" -> "/app";
                case "getParameter" -> parameters.get((String) args[0]);
                case "setAttribute" -> null;
                case "getRequestDispatcher" -> {
                    String path = (String) args[0];
                    yield proxy(RequestDispatcher.class, (n, a) -> {
                        if (n.equals("forward")) {
                            forwardedTo = path;
                        }
                        return null;
                    });
                }
                default -> null;
            });
        }

        public HttpServletResponse response() {
            return proxy(HttpServletResponse.class, (name, args) -> {
                switch (name) {
                    case "sendRedirect" -> redirectedTo = (String) args[0];
                    case "setStatus" -> status = (Integer) args[0];
                    default -> { }
                }
                return null;
            });
        }

        public FilterChain chain() {
            return (req, res) -> chainCalled = true;
        }
    }

    interface Handler {
        Object handle(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, method, args) -> {
            Object result = handler.handle(method.getName(), args);
            if (result == null && method.getReturnType().isPrimitive()) {
                return method.getReturnType() == boolean.class ? Boolean.FALSE : (Object) 0;
            }
            return result;
        });
    }
}
