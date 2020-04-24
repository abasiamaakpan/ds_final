#!/usr/bin/env bash
#
# Start-up Script
# this script starts all 5 servers with predefined ports
# then starts the client with matching ports
# run this from the base directory ./ with included ./jar directory+files
#
# @auth Neil Routley, Sanchit Saini, Abasiama Akpan
# @since 03/24/2020

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java PaxosServer 9090 9091 9092 9093 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java PaxosServer 9091 9090 9092 9093 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java PaxosServer 9092 9090 9091 9093 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java PaxosServer 9093 9090 9091 9092 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java PaxosServer 9094 9090 9091 9092 9093"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java PaxosClient 9090 9091 9092 9093 9094"
END