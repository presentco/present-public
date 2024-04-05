App Engine Default Module
=========================

Uses App Engine's standard environment. Implements Bubble's web app.

Editing HTML
------------

To quickly iterate on HTML only:

    cd java/appengine-web/src/main/webapp
    python -m SimpleHTTPServer 8000

Requesting an Invitation
------------------------

    POST /rest/requestInvitation?firstName=[first name]
      &lastName=[last name]
      &email=[email]
      &zip=[zip]
      
    Returns: 200 (no body)

    Example:
    
    curl -X POST -d "firstName=Bob&lastName=Lee&zip=94158&email=bob@present.co" \
      http://localhost:8080/rest/requestInvitation

CSS Generation
--------------

- initial install

    ```sh
    npm install --global postcss-cli autoprefixer
    ```

- run before pushing, in `present/java/appengine-web/src/main/webapp` dir
    ```sh
    postcss styles/stylesheet-src.css --use autoprefixer -o styles/stylesheet.css
    ```

- only edit the css src file
