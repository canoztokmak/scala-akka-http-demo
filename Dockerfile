FROM hseeberger/scala-sbt

COPY . ~/movie-reservation
WORKDIR ~/movie-reservation

EXPOSE 9000

CMD ["sbt", "run"]
