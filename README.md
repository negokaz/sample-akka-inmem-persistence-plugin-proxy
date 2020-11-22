# sample-akka-inmem-persistence-plugin-proxy

A sample project to describe how to use [Persistence Plugin Proxy](https://doc.akka.io/docs/akka/2.6/persistence-plugins.html#persistence-plugin-proxy) with [akka-persistence-inmemory](https://github.com/dnvriend/akka-persistence-inmemory)

## Usage

You can confirm a failing test if it doesn't enable PersistencePluginProxy.

```bash
git checkout main   # PersistencePluginProxy is disabled
sbt "multi-jvm:testOnly *.BankAccountServiceSpec"
```

Enable PersistencePluginProxy to make successful the test.

```bash
git checkout use-persistence-plugin-proxy-in-test   # PersistencePluginProxy is enabled
sbt "multi-jvm:testOnly *.BankAccountServiceSpec"
```

You can check diff to know how to enable PersistencePluginProxy.

```bash
git diff main use-persistence-plugin-proxy-in-test
```
