# concurrentProject
This is our Project for EE360P Spring 2017. It is an android chatroom.
The team is comprised of Justin Curewitz, Kristian Wang, Chris Chen, Pascale Queralt.

Assignment Blurb:

Develop distributed applications for Android/iPhone. Here are some suggestions: (i) Global scheduling: A set of friends download the app. They use the app to determine first available meeting time. (ii) Chatroom (iii) Lunch invite (iv) survey (v) multiplayer games. (vi) an app that keeps useful information about the department. For example, it may allow an user to check seminars on any given day or to reserve a computer workstation. (vii) an app for multiplayer game. Add a tutorial on developing mobile apps. Your app must use either multiple threads or some distributed computing ideas discussed in this course.




Some ideas on division of labor

App Side:
- GUI
- Client Logic to send JSON messages to Server

Server Side:
- Where the distributed logic lives
- Everyone receives an updated state of the chatroom continuously from Server
- Logic to handle JSON and send back messages
