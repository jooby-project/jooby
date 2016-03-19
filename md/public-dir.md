# static resources

This isn't a deployment option, just a way to collect static resources and deploy them somewhere else.

The ```static``` assembly let you collect and bundle all your static resources into a ```zip``` file.

This is nice if you want or prefer to serve static resources via {{nginx}} or {{apache}}.

## usage

* Write a ```assets.activator``` file inside the ```src/etc``` directory

* Open a console and type: ```mvn clean package```

* Look for ```[app-name].static.zip``` inside the ```target``` directory
