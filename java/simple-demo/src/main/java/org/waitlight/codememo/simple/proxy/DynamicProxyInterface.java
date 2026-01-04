package org.waitlight.codememo.simple.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DynamicProxyInterface {

    public static void main(String[] args) {
        Subject proxy = (Subject) Proxy.newProxyInstance(
                Subject.class.getClassLoader(),
                new Class[]{Subject.class},
                new ProxyHandler()
        );
        System.out.println(proxy.sayHello());
        System.out.println(proxy.sayBye());
    }

    interface Subject {
        String sayHello();

        String sayBye();
    }

    static class ProxyHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("Into proxy handler...");
            return "Call %s method success...".formatted(method.getName());
        }
    }
}
