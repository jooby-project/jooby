# Jooby Javadoc (Internal Module)

> ⚠️ **STOP: INTERNAL USAGE ONLY** ⚠️
>
> **This module is strictly for internal use by the Jooby framework. It is NOT intended for public consumption or end-user applications.**

## 🛑 Do Not Build Against This API

Please be aware that the classes and interfaces within this module are considered **private internal implementation details**. We strongly discourage developers from writing code that directly depends on this module.

If you choose to use this module in your own projects, you do so entirely at your own risk. Please note:

* **No Backward Compatibility:** We do not follow semantic versioning for these internal packages.
* **Sudden Breaking Changes:** APIs, method signatures, and entire classes can (and will) be modified, renamed, or completely removed at any time without prior notice or deprecation cycles.
* **No Public Support:** Issues or feature requests related to direct third-party usage of this module will be closed.

## What is this for?
This module contains the abstract syntax tree (AST) extraction logic used internally to parse Javadoc comments. It exists solely to power the official `jooby-openapi` generator.

If your goal is to generate OpenAPI documentation for your application, please depend on the official, public-facing `jooby-openapi` module instead.

---
*If you are a core contributor actively working on Jooby's OpenAPI generation pipeline, you are in the right place.*
