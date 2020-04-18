#!/usr/bin/env bash
#
# 2Phase Commit Start-up Script
# this script starts all 5 servers with predefined ports
# then starts the client with matching ports
# run this from the base directory ./ with included ./jar directory+files
#
# @auth Neil Routley
# @since 03/28/2020

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/PaxosServer.jar 9090 9091 9092 9093 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/PaxosServer.jar 9091 9090 9092 9093 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/PaxosServer.jar 9092 9090 9091 9093 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/PaxosServer.jar 9093 9090 9091 9092 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/PaxosServer.jar 9094 9090 9091 9092 9093"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/PaxosClient.jar 9090 9091 9092 9093 9094"
END

# osascript <<END 
# tell app "Terminal" to do script "cd \"`pwd`\" && java TwoPhaseServer 9090 9091 9092 9093 9094"
# END

# osascript <<END 
# tell app "Terminal" to do script "cd \"`pwd`\" && java TwoPhaseServer 9091 9090 9092 9093 9094"
# END

# osascript <<END 
# tell app "Terminal" to do script "cd \"`pwd`\" && java TwoPhaseServer 9092 9090 9091 9093 9094"
# END

# osascript <<END 
# tell app "Terminal" to do script "cd \"`pwd`\" && java TwoPhaseServer 9093 9090 9091 9092 9094"
# END

# osascript <<END 
# tell app "Terminal" to do script "cd \"`pwd`\" && java TwoPhaseServer 9094 9090 9091 9092 9093"
# END

# osascript <<END 
# tell app "Terminal" to do script "cd \"`pwd`\" && java TwoPhaseClient 9090 9091 9092 9093 9094"
# END