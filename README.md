************************************************************************
                           TB Monitor Framework
               Luke Geneslaw, Karthic Aragam, and Paul Chang
************************************************************************

1. Introduction
2. Installation
3. Using the framework
4. Future

************************************************************************
                             1. Introduction
************************************************************************
    This project was meant to suppliment the research and work being
done at the University of Lima, led by PhD candidate German Comina. The
TB Monitor Framework is meant to help TB patients monitor their health
by tracking the number of times they cough throughout the treatment
process and showing them their trends in an app. As their health
improves, they will notice a decrease in their cough rates. The
framework also includes a module called the sanitizer that is used to
help record quality audio. The sanitizer is an open source python
project.

************************************************************************
                             2. Installation
************************************************************************
    The TB Monitor app is currently only on Android, so to install it
simply put the tbmonitor.apk on your android device and install.
    As for the sanitizer, the only system requirement is to have python
installed. You can download the sanitizer from github at
https://github.com/karagam123/Audio-Sanitizer.

************************************************************************
                          3. Using the framework
************************************************************************
    To begin tracking your health with TB Monitor, open the app and
hit "Inscribirse," then enter your registration info. You now have an
account, so when you login you can view graphs of your cough counts over
various periods of time. Initially, you won't have any data for the
graph to display. Once you upload some cough data, you can use the tabs
dscroll through the buttons below the graph to view past data.
    If you would like to see data displayed, you can use our test
account to login.

    email : lgeneslaw@gmail.com
    password : 12345

    Note: recording cough data is out of the scope of this project,
because ultimately this project will be handed off to the team in Peru
and paired with the recording technology that they have been developing.
    
    In order to use the Sanitizer on a wav file, the name of the wav file
must be hard-coded into "Sanitizer.py". The Sanitizer then outputs a wav
file with a name that can also be hard-coded. To run the sanitizer:
    $ python sanitizer.py

************************************************************************
                               4. Future
************************************************************************
    There are several improvements that will be made to this project by
the Peru team in the future, as well as some potential features that
would further improve the benefit of the TB Monitor Framework. Below is
a list of future improvements in relative order of urgency.

  - Adding audio recording to the front end is crucial. Currently, the
    backend provides a Web API that is capable of receiving audio files
    via HTTP requests, but there is no native way to record and send
    files to the server at this point.
  - Adding the cough detection algorithm to the server is also necessary
    for the usability of the framework. The Peru team has a Matlab cough
    detection algorithm that they will merge with our backend so it can
    not only receive audio files but also process them. Currently, if
    the Web API receives and audio file for a patient, it does update
    their data and new information can be viewed in the app, but the
    data does not reflect the actual number of coughs found in the
    patient's audio file.
  - Adding security to the interactions to the interaction between the
    app and the backend is also necessary. Currently, the login and
    registration process has some security built in, but this can be
    improved, and security needs to be added to Web API requests and
    responses.
