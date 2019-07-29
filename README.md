# GitPals

<img src="https://image.flaticon.com/icons/svg/89/89341.svg" width="150" height="150">

Application for finding people to work together with on different projects

[Open GitPals app](https://gitpals.herokuapp.com/) **!!! It can take up to 60 seconds to open GitPals. If you see an error (404 project not found) when it's opened - refresh your page**

This project uses **Java, Spring, MongoDB**

![image](gallery/main.png)
![image](gallery/d1.png)
![image](gallery/d2.png)
![image](gallery/project1.png)
![image](gallery/messages.png)

# Contributing:
Thank you for deciding to contribute! Download GitPals to your PC. You need to have MongoDB installed on your pc.
Open project in any IDE (I use Intellij IDEA). Then open Console Prompt and type in: mongod --dbpath=path_to_db (for example: mongod --dbpath=C:\GitPals\DB). 

[Video Guide](https://youtu.be/JbvxJyXmOEM)

**address - localhost:1337**

**MongoDB port - default 27017**

**Unless you run mongo database, web page will return error**

Executing file - GitPals/src/main/java/com/moople/gitpals/MainApplication.java

**Controller folder** is a main folder. Controllers execute all the functions (register user, add project, delete smth etc.)

**Service folder** contains just 2 files. That files save data to Mongo Database

**Model folder** contains files that have information about each object (user's name, project's title etc.)
