# binary-proxy

## Motivation

Sometimes embedded devices have very high requirements to traffic cost. And we can't use common practices like REST, SOAP. Moreover they usually have memory restrictions and can't support of them. There are many solutions of this problem. One solution is thin proxy which encode binary message to base64 and send to REST backend. It has a couple advantages - backend servers can be more universal and you can use any comfortable programming language or framework on them.

## Example

Device connected to proxy and sent a binary message with payload "HELLO, WORLD":
 Original message (14 bytes)    Send to backend (base64("HELLO, WORLD")-> "SEVMTE8sIFdPUkxE")
 +--------+----------------+      +------------------+
 | Length | Actual Content |----->| POST /target/    |
 | 0x000C | "HELLO, WORLD" |      |"SEVMTE8sIFdPUkxE"|
 +--------+----------------+      +------------------+
 
Backend returned "SGVsbG8="("Hello"), proxy decoded content and returned to device binary message:
 +--------+----------------+      +------------------+
 | Length | Actual Content |<-----| POST /target/    |
 | 0x0005 |    "Hello"     |      |    "SGVsbG8="    |
 +--------+----------------+      +------------------+

## How to build

You require the following to build Netty:

* Latest stable [Oracle JDK 8](http://www.oracle.com/technetwork/java/)
* Latest stable [Gradle](https://gradle.org/)

