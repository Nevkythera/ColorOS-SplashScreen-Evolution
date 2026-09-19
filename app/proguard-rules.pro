# libxposed API 102 —— 模块混淆规则
# 保留模块入口类及其构造方法，避免被 R8 移除或重命名

# 保留所有继承 XposedModule 的入口类的 public 构造（框架通过反射实例化）
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>(...);
}

# libxposed 注解不产生警告
-dontwarn io.github.libxposed.annotation.**

# 若入口类被混淆，自动重写 java_init.list 中的类名
-adaptresourcefilecontents META-INF/xposed/java_init.list
