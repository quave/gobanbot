FROM clojure:lein

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

RUN apt-get update && apt-get upgrade -y && apt-get install build-essential -y
RUN wget http://ftp.gnu.org/gnu/gnugo/gnugo-3.8.tar.gz && \
  tar -xvf gnugo-3.8.tar.gz && \
  cd gnugo-3.8 && \
  ./configure --enable-level=9 && \
  make && make install

RUN cd /home && wget http://downloads.sourceforge.net/project/boost/boost/1.60.0/boost_1_60_0.tar.gz \
  && tar xfz boost_1_60_0.tar.gz \
  && rm boost_1_60_0.tar.gz \
  && cd boost_1_60_0 \
  && ./bootstrap.sh --prefix=/usr/local --with-libraries=program_options \
  && ./b2 install \
  && cd /home \
  && rm -rf boost_1_60_0

RUN git clone https://github.com/online-go/score-estimator && \
  cd score-estimator && make

COPY project.clj /usr/src/app/

RUN mkdir -p /usr/share/fonts
COPY ./open-sans-condensed.light.ttf /usr/share/fonts
RUN fc-cache -fv

RUN lein deps
COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

CMD ["java", "-jar", "app-standalone.jar"]
