package org.waitlight.codememo.simple.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DynamicProxyClass {
    public static void main(String[] args) {
        Subject subject = new RealSubject();
        Subject proxy = (Subject) Proxy.newProxyInstance(
                subject.getClass().getClassLoader(),
                subject.getClass().getInterfaces(),
                new ProxyHandler(subject)
        );

        System.out.println(proxy.sayHello());
        System.out.println(proxy.sayBye());
    }

    interface Subject {
        String sayHello();

        String sayBye();
    }

    static class RealSubject implements Subject {
        @Override
        public String sayHello() {
            System.out.println("Real Subject is handling request......");
            return "hello";
        }

        @Override
        public String sayBye() {
            System.out.println("Real Subject is handling request......");
            return "bye";
        }
    }

    static class ProxyHandler implements InvocationHandler {
        private final Object target;

        public ProxyHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("Before %s requesting...".formatted(method.getName()));
            Object result = method.invoke(target, args);
            System.out.println("After %s requesting...".formatted(method.getName()));
            return result;
        }
    }
}
