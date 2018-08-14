FROM clojure:lein

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY project.clj /usr/src/app/

RUN mkdir -p /usr/share/fonts
COPY ./open-sans-condensed.light.ttf /usr/share/fonts
RUN fc-cache -fv

RUN lein deps
COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

CMD ["java", "-jar", "app-standalone.jar"]
