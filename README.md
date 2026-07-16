# Lombok Refactor Enterprise Maven Plugin

Maven plugin that safely replaces **trivial Java getters and setters** with field-level **Lombok `@Getter` and `@Setter` annotations**, generating a detailed HTML report of changes.

---

## ✨ Features

- Replaces only **trivial** getters and setters
- Adds `@Getter` / `@Setter` on **specific fields** (not entire classes)
- Preserves methods that contain custom logic
- Generates **HTML refactoring report**
- Creates `.bak` backup files for modified sources
- Does not use `@Data`
- Safe for partial migrations
- Thread-safe Maven goal

---

## 🔎 What is considered a trivial method?

### Getter must be exactly:

```java
public Type getField() {
    return field;
}
```

or

```java
public Type getField() {
    return this.field;
}
```

---

### Setter must be exactly:

```java
public void setField(Type field) {
    this.field = field;
}
```

or

```java
public void setField(Type field) {
    field = field;
}
```

If a method contains:
- final or static attribute

➡ it will **NOT** be modified.

---

## 🛠 Installation

### 1 Add plugin to your target project

In the target project's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>pl.tomekkup</groupId>
            <artifactId>lombocator-maven-plugin</artifactId>
            <version>1.0.2</version>
            <executions>
                <execution>
                    <goals>
                        <goal>refactor</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <sourceDirectory>${project.basedir}/src/main/java</sourceDirectory>
                <reportFile>${project.build.directory}/lombok-report.html</reportFile>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

### 2 Run the plugin

```bash
mvn process-sources
```

or directly:

```bash
mvn lombok-refactor:refactor
```

---

## 📄 Output

### ✔ Modified source files

- Trivial getters/setters removed
- `@Getter` / `@Setter` added on matching fields
- Backup files created:

```
MyClass.java.bak
```

### ✔ HTML Report

Generated at:

```
target/lombok-report.html
```

Includes:
- modified classes
- removed methods
- annotated fields

---

## 📦 Required Dependency (Target Project)

Your project must include Lombok:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <scope>provided</scope>
</dependency>
```

---

## 🏗 Project Structure

```
lombok-refactor-enterprise/
 ├─ pom.xml
 └─ src/
     └─ main/
         ├─ java/
         │   └─ com/example/lombokrefactor/
         │       └─ LombokRefactorMojo.java
         └─ resources/
             └─ META-INF/maven/
                 └─ plugin.xml (optional)
```

---

## 🔐 Safety Design

This plugin:

- Uses AST parsing via JavaParser
- Validates method body structure before refactoring
- Does not rely on regex
- Does not touch non-trivial methods
- Does not annotate entire classes
- Does not introduce `@Data`

---

## ⚠ Limitations

- Does not currently support fluent accessors
- Does not support chained setters
- Does not analyze inheritance hierarchy
- Does not support records
