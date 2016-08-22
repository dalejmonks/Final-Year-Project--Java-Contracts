# Final-Year-Project--Java-Contracts
This was my 3rd year project, a look at java's meta programming and method intercepting.
About the topic of formal specification and checking.
It generates a library which can be used with any project.

It uses the annotations I created in order to later process them at runtime.
So long as the class was created using my Contract Contstructor class.

It uses the javassist library in order to intercept the objects method calls.

As well as using Java 8 features like the lambda function / functional java. 
Using JavaScript within a scriptengine within Java in order to evaluate the annotations string.

Side Note:
I had to use annotations for method parameter names as they get thrown away, but used then with the memeber variables as it seemed more of a convention.
