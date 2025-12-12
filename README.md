# Internet-of-Things-IoT-Device-Simulator-
📡 Internet of Things (IoT) Device Simulator

A Java-based IoT Simulator that mimics real-world IoT devices by generating virtual sensor data and displaying real-time dashboards. It demonstrates OOP, multithreading, Swing GUI, and simulated device-to-server communication without needing actual hardware.

🚀 Features

Temperature & Motion Sensor Simulation

Real-time Graphs and Live Data Visualization

Device Dashboard with Logs

Multithreaded Data Generation

Fullscreen Java Swing UI

Start/Stop IoT Server

Sensor Selection Screen

🛠️ Technologies Used

Java (Core + OOP)

Java Swing (GUI)

Multithreading

Executors & Scheduled Tasks

ArrayDeque Data Buffers

Custom Components (Cards, Graph Panels)

📂 Project Structure
IoTProjectPerfecttt.java
 ├── Start Screen  
 ├── Dashboard  
 ├── Temperature & Motion Panels  
 ├── Value Cards  
 ├── Logs & Device List  
 └── Custom UI Components

📸 Screenshots (Add later)

Start Screen

Dashboard

Temperature Graph

Motion Graph

(Upload images after creating repo)

🧠 How It Works

User selects sensors

Simulator creates virtual IoT devices

Each device produces continuous data

Data is displayed in real-time graphs

Logs show all activity

Server can be started or stopped anytime

📘 Installation & Run
javac IoTProjectPerfecttt.java
java IoTProjectPerfecttt

📑 ER Diagram (Very Short)

Server → Device = One-to-Many

Device → SensorData = One-to-Many

🔄 DFD Summary (Very Short)

Level 0: User interacts with IoT Simulator
Level 1: Select sensors → Generate data → Process data → Show graphs
Level 2: Initialize sensor → Produce data → Send to server → Update dashboard

👤 Author

Avinash Singh
B.Tech IT (2024–2028)

