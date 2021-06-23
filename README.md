# HTTPServerAndClientApplication

Implemented my own HTTP server and client library with Java socket APIs using two different techniques: TCP protocol to guarantee packet transmission over unreliable network links, and UDP protocol with Selective Repeat ARQ / Selective Reject ARQ to make a reliable transport, and to support simultaneous multi-requests; then developed it to a command-line application similar to CURL.

Practiced hard coding low-level details of network including HTTP protocol, TCP and UDP Socket API, addressing, sending/receiving network packets, logical router, peer address and peer Port, payload.
