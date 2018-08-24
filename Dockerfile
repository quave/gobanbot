FROM clojure:lein

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

RUN apt-get update && apt-get upgrade -y && apt-get install build-essential -y
RUN wget http://ftp.gnu.org/gnu/gnugo/gnugo-3.8.tar.gz && \
  tar -xvf gnugo-3.8.tar.gz && \
  cd gnugo-3.8 && \
  ./configure --enable-level=9 && \
  make && make install

COPY project.clj /usr/src/app/

RUN mkdir -p /usr/share/fonts
COPY ./open-sans-condensed.light.ttf /usr/share/fonts
RUN fc-cache -fv

RUN lein deps
COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

CMD ["java", "-jar", "app-standalone.jar"]
