
// Shows runtime information about all SoapProcessors
// It is named after the ps command in Unix

ps = { sys -> 
  print String.format('%1$15s\t%2$31s\t%3$7s', "ORGANIZATION", "SOAP PROCESSOR", "STATUS")
  println String.format('\t%1$7s\t%2$7s\t%3$15s\t%4$9s', "PID", "BUSDOCS", "TOTAL", "LAST") 
  for (sp in sys.sp.sort()) { 
    print String.format('%1$15s\t%2$31s\t%3$7s', sp.parent.parent.name, sp.name, sp.status)
    if (sp.pid==-1)
      println()
    else 
      println String.format('\t%1$7d\t%2$7d\t%3$13dms\t%4$7dms', sp.pid, sp.busdocs, sp.processingTime, sp.lastTime) 
  }
}
