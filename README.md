VoLTE_Dialer
============

Android App to test VoLTE service KPIs

This application can be used to send or receive calls in order to measure:

1. call setup time.
2. call failure ration during establishment.
3. call dropped ration. I.e. mid-call disconnections
4. amount of calls that experience SRVCC handover.

This application is intended for "operation" teams within mobile operators.

This application has two compilation options:

a. It can be compiled to work in normal Android handsets, with limited logging functionality.
b. It can be compiled to work in rooted Android handsets. In this case, the app uses java reflection to get access to
Android internal telephony functions, such as precise call states or SRVCC events.

Both compilations have been tested in Samsung Galaxy S5 with special VoLTE FW from Samsung.

