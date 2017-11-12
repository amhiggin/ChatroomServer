# ChatroomServer
## Amber Higgins, MAI Computer Engineering 2017/2018, 13327954
* For Scalable Computing module

# Dependencies

## Runtime Dependencies
<i><b> Included in the /libs dir</i></b>
* JRE 1.7.0_151
* Joda Time 2.9.9
* Apache Commons IO 2.6

## Test Dependencies ##
<i><b> Included in the /testlibs dir</i></b>
* Mockito v1.9.5
* JUnit v4.12

# Deployment Instructions
1. Clone this repository using <i><b> git clone https://www.github.com/amhiggin/ChatroomServer </b></i>.
2. Run the <u>compile.sh</u> script:
    * Assign execute permissions using '<i><b>chmod 755 compile.sh</i></b>'. (don't forget <i>sudo</i> if necessary).
    * Run using '<b><i>./compile.sh</b></i>'. 
3. Run the <u>start.sh</u> script:
    * As before, assign execute permissions using '<b><i>chmod 755 start.sh</b></i>'.
    * Run the script, specifying the port number you want the server to be accessible at. This looks something like
     '<i><b>./start.sh <port_number></i></b>'.
    * You should see a message similar to the following:
  <b><i> 01/11/2017 15:31:52>> Server started on port 1234...</i></b>
