FROM hseeberger/scala-sbt

COPY . ~/scala-akka-http-demo
WORKDIR ~/scala-akka-http-demo

EXPOSE 9000

CMD ["sbt", "run"]
