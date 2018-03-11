Simple CV Generator
===================

This is a simple project that generate PDF CV from templates in XHTML/CSS 2.1 and YAML data format.

The generated jar include all dependencies you need. You just have to make sure that you have a java 8 runtime installed.

To use it:
```
java -jar simple-cv-generator.jar
```
This command will use default value to generate cv which are:

- `cv.yml` in working directory for you cv data
- `cv.pdf` for the generated pdf output
- `default` theme which is in `themes/default/index.html`

If you want to use other values you can use the jar as follow:

```
java -jar simple-cv-generator.jar --theme mytheme mycv.yml mycv.pdf
```

or

```
java -jar simple-cv-generator.jar -t mytheme mycv.yml mycv.pdf
```

You can omit each argument which will be resolved as default values. The argument order is not important, the YAML and PDF argument are resolved by the file extension.

Note that the theme has to be either in the classpath of the jar or in working directory in `themes/mytheme/index.html`.

The CV YAML format can be specific to theme.