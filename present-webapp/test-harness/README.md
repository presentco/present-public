
# Test Harness
server.py reads JSON from server.json and applies it to mustache syntax (https://mustache.github.io/) in the served html files.

# Use homebrew
If you have not used brew before you will need to install it and do the following:
    sudo mkdir /usr/local/lib/Frameworks
    sudo chown -R $USER /usr/local/lib/*

# Install python and pystache
brew install python # installs python2 and pip2 into /usr/local/bin
pip2 install pystache

# Running
./server.py
Point your browser at localhost:8000/test.html

Or run render-test.py to render the test.html to stdout.

