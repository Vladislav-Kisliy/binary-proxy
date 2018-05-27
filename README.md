# binary-proxy

## Motivation

Sometimes embedded devices have very high requirements to traffic cost. And we can't use common practices like REST, SOAP. Moreover they usually have memory restrictions and can't support of them. There are many solutions of this problem. One solution is thin proxy which encode binary message to base64 and send to REST backend. It has a couple advantages - backend servers can be more universal and you can use any comfortable programming language or framework on them.

## How to build

You require the following to build Netty:

* Latest stable [Oracle JDK 8](http://www.oracle.com/technetwork/java/)
* Latest stable [Gradle](https://gradle.org/)

