# RedDash

Backend dashboard to display the status of RedWire and RedMetrics servers. 

Written in Clojure.


## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen


## Running

To start a web server for the application, run:

    lein ring server

To make an uberjar for easy deployment, run

    lein ring uberjar
    java --add-modules java.xml.bind -jar target/red-dash-0.1.0-SNAPSHOT-standalone.jar


## License

MIT License

Copyright © 2018 Jesse Himmelstein
