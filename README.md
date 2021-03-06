[![Build Status](https://travis-ci.org/BEinCPPS/fitman-cam.svg?branch=master)](https://travis-ci.org/BEinCPPS/fitman-cam)

#Fitman-cam project

###Description of the component

The **Collaborative Asset Management (CAM)** Specific Enabler is a web-based, integrated platform for the management of Virtualized Asset. This Specific Enabler is targeted to the business user, who is not required to have IT expertise, nor an in-depth knowledge of ontology-related concepts and technologies.

CAM is based on several open source components, covering different functional areas; on top of these, it adds a rich layer of web-based, custom front-end functionalities which integrates low-level services into a unified, user-friendly experience.

CAM is delivered as a module that allows the user to manipulate ontologies adding classes and templates and create, manipulate and delete Assets.


This CAM release contains two modules, **Cam** and **Cam-Service**.
 
**Cam**

Cam is a web application that exploits cam-service APIs. Cam allows user to create, manipulate and delete Assets using a web interface.

**Cam-Service**

Cam-Service component exposes its own public, proprietary REST-based web API. By means of API calls, the reference ontology, the asset repository and the service registry can be queried by external applications. The usual CRUD operations will be allowed on Class, Assets, Owners and Attributes.
	
###Developer environment

This procedure assumes that you have [Apache Tomcat](https://tomcat.apache.org/download-80.cgi) (version >= **7**)
and [RDF4J 2.0M2](http://rdf4j.org/download/) installed in your environment.

Before start using application you must have RDF4J up and running, we also need to create a new repository in it. 
We will refer to this repository as ```<EXAMPLE_REPO>```.

• Open a web browser and navigate to your rdf4j:
 ```
 <host>:<port>/rdf4j-workbench/
 ```

• Click on new repository, on top left, and fill id and title.<br/>
• Click next, select persistence mode and finally click finish.<br/>

If creation is successful the user will be redirected to repository summary.


**A.** Your project structure is as follows: <br/>

```
<your_project_dir>
   |__ cam
   |__ cam-service
```

**B.** Install Cam: <br/>

1.	Open a terminal window and go to the root folder of CAM project .
2.	Type the command: mvn package.
3.	Copy the war in ```target/``` to ```<PATH_TO_TOMCAT>/webapps```.
4.	Browse to ```<YOUR_HOST>:<YOUR_PORT>/CAM ``` to start using application.

**C.** Install Cam-Service:<br/>
```bash
$ cd CAMService
$ mvn package -P prod
```

To skip Unit Tests use ``-DskipTests`` maven parameter.

**D.** Integration Test (This test uses **Sesame Repository in Memory** and **Apache Tomcat 7 Maven embedded**):

```bash
$ cd CAMService
$ mvn package
$ mvn verify 
```

The default port in order to use CAMService with Sesame repo is 8080, feel free to change this parameter inside the file pom.xml.

Change sesame repository properties with your sesame installation: 

```bash
sesame.url
sesame.repository (<EXAMPLE_REPO>)
sesame.namespace
```

Copy the CAMService.war into a Tomcat installation.

```bash
$ cp ./CAMService/target/CAMService.war ./apache-tomcat-8.0.33/webapps
```

**Note**: This project uses [travis-ci](https://travis-ci.org/) for continuous integration.
