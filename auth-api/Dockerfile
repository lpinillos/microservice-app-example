FROM golang:1.18 AS builder
WORKDIR /app
COPY . .
RUN go build -o main .

FROM golang:1.18
WORKDIR /app
COPY --from=builder /app/main .
ENV AUTH_API_PORT=8000
EXPOSE 8000
CMD ["./main"]
