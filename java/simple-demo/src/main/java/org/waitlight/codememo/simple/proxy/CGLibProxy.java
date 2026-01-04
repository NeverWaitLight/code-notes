package org.waitlight.codememo.simple.proxy;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class CGLibProxy {
    /**
     * VM Option 添加 "--add-opens java.base/java.lang=ALL-UNNAMED" 这将允许 CGLib 访问 java.lang.ClassLoader.defineClass 方法
     *
     * @param args
     */
    public static void main(String[] args) {
        UserService userService = new UserService();
        UserService proxy = (UserService) new UserServiceProxy(userService).createProxy();

        System.out.println(proxy.sayHello());
    }

    static class UserService {
        public String sayHello() {
            return "Hello";
        }
    }

    static class UserServiceProxy implements MethodInterceptor {
        private final Object target;

        public UserServiceProxy(Object target) {
            this.target = target;
        }

        public Object createProxy() {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(target.getClass());
            enhancer.setCallback(this);
            return enhancer.create();
        }

        @Override
        public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            System.out.println("Before method execution......");
            Object result = method.invoke(target, args);
            System.out.println("After method execution......");
            return result;
        }
    }
}
