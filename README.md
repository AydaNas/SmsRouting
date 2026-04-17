
# Installation

## Clone repogitry
To run the project, first clone this repository.
```
git clone https://github.com/AydaNas/SmsRouting.git
cd SmsRouting
```


## Run RabbitMQ
if you are using Docker:

```
docker run -it --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

Then open a web browser and access to RabbitMQ
```
http://localhost:15672/
```
The default user name and password are “guest”, “guest”.

## Run project codes manually

### Set classpath
Set the classpath correctly.
```
export CLASSPATH=$CLASSPATH:<yourpath>/classes
```

### Compile codes
Compile all project files.
```
javac -d classes -classpath "lib/*" src/Erreur/* src/utils/* src/read_network/* src/User.java src/Antenna.java src/SMSRouting.java
```

### Run Antennas
Run antennas before running users with a configuration file.
```
java -cp "classes:lib/*" SMSRouting debug antenna ressource/<configuration file>.net <antenna name>
```

For instance:
```
java -cp "classes:lib/*" SMSRouting debug antenna ressource/exemple_2.net A
```

### Run Users
Run users with coordinates(x,y).
```
java -cp "classes:lib/*" SMSRouting debug user <user name> <coordinate(x)> <coordinate(y)>
```

For example:
```
java -cp "classes:lib/*" SMSRouting debug user user1 0 2
```

## User Interaction

### Send a message
```
send <Destination> <message>
```

For exapmle:
```
send user2 hello
```

### Move local position
```
move <x_position> <y_position>
```

For example:
```
move 0 5
```

### Disconnect
```
exit
```
