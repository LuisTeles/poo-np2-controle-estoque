# How the TCP connection works in TraceCore

This project uses **two separate Java applications**:

1. **Factory Core** - `com.fabrica.app.Main`
2. **Monitoring Terminal** - `com.fabrica.tcp.MonitorMain`

They run in **different terminals** and communicate through a **TCP connection** on the local machine.

## Why TCP was used

TCP was chosen because it is a simple and reliable way for two independent programs to exchange messages in real time.

In this project, that is useful because:

- the **monitor terminal** can stay open only to listen for alerts;
- the **factory terminal** can keep the CLI menu responsive for the user;
- the alert message is delivered from one process to the other without mixing both responsibilities in the same terminal;
- TCP guarantees ordered and reliable delivery, which is important for stock alerts.

So, instead of trying to "split" one terminal into two views, the system creates **two processes** and lets them talk over the network stack of the computer.

## How the connection is organized

The communication uses the classic **client/server socket model**:

- `MonitorMain` starts a `ServerSocket` on port `5050`;
- `Main` uses `TcpAlertClient`, which creates a `Socket` to `127.0.0.1:5050`;
- when stock becomes critical, the factory sends an alert message;
- the monitor receives the message and prints it immediately.

Even though both programs run on the same machine, TCP still works the same way as it would across a network.

## Detailed flow

### 1. Monitor starts first

When you run:

```bash
java -cp out com.fabrica.tcp.MonitorMain
```

the monitor creates a `ServerSocket`.

That means:

- it reserves port `5050`;
- it stays blocked waiting for incoming connections;
- it does not execute factory logic;
- it only receives and displays alerts.

This terminal is dedicated to monitoring.

## 2. Factory application starts

When you run:

```bash
java -cp out com.fabrica.app.Main
```

the factory CLI starts normally and also creates:

- the inventory service;
- the persistence layer;
- the stock monitoring thread;
- the TCP alert client.

The user keeps interacting with the menu in this terminal.

## 3. Background thread checks stock

The class `StockMonitorThread` runs in the background.

Its job is to:

- periodically inspect the inventory;
- detect items that reached the critical threshold;
- avoid repeating the same alert continuously while the item remains critical.

This is why the CLI and monitoring happen "at the same time":

- the **main thread** handles user input in the menu;
- the **monitor thread** checks inventory in parallel;
- if needed, it sends a TCP alert without blocking the menu flow.

## 4. Factory opens a TCP connection only when needed

When the background thread finds a critical stock condition, it creates a `StockAlert` and calls `TcpAlertClient`.

`TcpAlertClient`:

1. opens a `Socket` to `127.0.0.1:5050`;
2. writes the alert payload to the socket output stream;
3. flushes the message;
4. closes the connection.

This design is simple and enough for the project because alerts are short messages and do not require a permanent connection.

## 5. Monitor receives and prints the message

On the monitor side, `MonitorServer` accepts the incoming socket connection.

For each connection, it starts a handler thread that:

- reads the incoming line;
- converts it back into a `StockAlert`;
- prints the formatted alert on the monitoring terminal.

Because the server accepts connections continuously, the monitor can receive multiple alerts over time while remaining active.

## Why two terminals are necessary

Two terminals are used because there are **two independent running programs**.

If both outputs were forced into the same terminal:

- the menu text and alert messages would mix together;
- the user experience would become confusing;
- the monitor would no longer behave like a separate external observer.

Using two terminals makes the architecture closer to a real distributed system:

- one side **produces events**;
- the other side **observes events**.

This separation is the main reason TCP fits well here.

## Why a thread is also necessary

TCP alone does not make things happen simultaneously.

The **thread** is what allows the factory application to do more than one thing at once inside its own process:

- keep reading menu actions from the user;
- keep checking whether stock became critical.

Without the thread, the program would need to stop the menu repeatedly to inspect the inventory, which would make the CLI less natural.

So the full concurrency model is:

- **two processes** -> two terminals;
- **TCP sockets** -> communication between those processes;
- **one background thread in the factory** -> continuous stock monitoring while the CLI keeps running.

## Summary

The project uses TCP because it is a reliable way for the factory terminal and the monitoring terminal to communicate as separate applications.

This makes it possible to keep:

- one terminal focused on user commands;
- another terminal focused on real-time alerts;
- a background thread watching stock levels without interrupting the CLI.

That combination is what allows the system to behave as a small distributed monitoring solution instead of a single terminal program.
