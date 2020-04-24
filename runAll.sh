#!/usr/bin/env bash
#
# 2Phase Commit Start-up Script
# this script starts all 5 servers with predefined ports
# then starts the client with matching ports
# run this from the base directory ./ with included ./jar directory+files
#
# @auth Neil Routley, Sanchit Saini, Abasiama Akpan
# @since 04/24/2020

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/FileServer.jar 9090 9091 9092 9093 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/FileServer.jar 9091 9090 9092 9093 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/FileServer.jar 9092 9090 9091 9093 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/FileServer.jar 9093 9090 9091 9092 9094"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/FileServer.jar 9094 9090 9091 9092 9093"
END

osascript <<END 
tell app "Terminal" to do script "cd \"`pwd`\" && java -jar jar/FileClient.jar 9090 9091 9092 9093 9094"
END
