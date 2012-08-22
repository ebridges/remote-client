#!/usr/bin/perl

# This is an XMLSocket multiuser server, for use with Flash 5
# If you don't know what XMLSocket is, you shouldn't be using
# this program.  

use IO::Socket;
use IO::Select;
#use DBI;
use lib '/perlmodules'; 
#use SMSMODULE;

my $logFileName =  "Socket_test.log" ;
		
		
#$myDbC = DBI->connect("DBI:mysql:sms_trivia", "root", "YS!mk!n") or myDie("can not connect to db");

# Set the input terminator to a zero byte string, pursuant to the
# protocol in the flash documentation.
$/ = "\0";


&startSocket();


###########################################################################################		
sub startSocket{		
	# Create a new socket, on port 10005
	my $PORT = 10005;
	print ("opening connection on port $PORT");
	$lsn = new IO::Socket::INET(Listen => 1, 
	    LocalPort => $PORT,
	    Reuse => 1,
	    Proto => 'tcp' );
	   #or die ("Couldn't start server: $!");
	
		
	# Create an IO::Select handler
	$sel = new IO::Select( $lsn );
	
	# Close filehandles
	
	close(STDIN); close(STDOUT);
	
	warn "Server ready.  Waiting for connections . . .  on \n";
	
			
	# Enter into while loop, listening to the handles that are available.
	while( @read_ready = $sel->can_read ) {
		$MESSAGE = 0;
		$fh  = $read_ready[0];
	
		# Create a new socket
		if($fh == $lsn) {
		    $new = $lsn->accept;
		    $sel->add($new);
		    push( @data, fileno($new) . " has joined.");
		    warn "Connection from " . $new->peerhost . ", listening on socket ". $new ."\n";
		    &logData("Connection from " . $new->peerhost);
		}
		
		# Handle connection
		else {
		    $input = <$fh>;
		    $input =~ s/\s+$//;
		    chomp $input;
		    warn "GOT INPUT '$input'";
		    &logData("GOT INPUT:$input from ". $fh);
		    
		    #just spit back whatever you receive
		    if($input ne ""){
		    	$MESSAGE = $input;
		    	$CLIENT_READY = 1;
		    }
		    
		    if($input eq "<policy-file-request/>"){
		    	
		    	$MESSAGE = 	qq~<?xml version="1.0"?>
									<cross-domain-policy>
									  <allow-access-from domain="*" to-ports="*"/>
									</cross-domain-policy>\0~;
				&logData("prepared MSG = $MESSAGE");	
				$CLIENT_READY = 1; 			
		    }
	    
	
	    	if ( $input eq '') {#disconnection notification by client
				warn "Disconnection from " . $new->peerhost . ".\n";	
				&logData("DISCONNECTING");			
				$sel->remove($fh);
				$fh->close;
		    }
		    
		    if ( $input eq "READY"){
		    	warn "CLIENT READY = 1\n";
		    	$CLIENT_READY = 1;
		    }
		    
		    if($input =~ m/PING/i){
		    	$MESSAGE = "PONG";
		    	$CLIENT_READY = 1;
		    }
		    
		}	    
	
	    # Write to the clients that are available
	    foreach $fh ( @write_ready = $sel->can_write(0) ) {
	    	&logData("IN LOOP");
	    	warn "outside send if fh=$fh, client_ready=$CLIENT_READY message=$MESSAGE\n";
	    	if($CLIENT_READY && $MESSAGE){
	    		warn ("sending $MESSAGE to $fh\n"); 
	    		$CLIENT_READY = 0;     		
				print $fh "$MESSAGE\0" or warn "can't send message to $fh"; 
				$MESSAGE = 0; 
	    	}     
	    }
	}
	warn "Server ended.\n";
}



 sub updateGameStateInDB{
# 	my ($inStr) = @_;
# 	warn "in update method with string $inStr\n";
# 	chomp($inStr);
# 	my $GS;
# 	my $GID;
# 	my @parts = split(/\&/, $inStr);
# 	foreach(@parts){
# 		@sides = split(/\=/, $_);
# 		if($sides[0] eq "GAMESTATE"){
# 			$GS = $sides[1];
# 		}
# 		if($sides[0] eq "GID"){
# 			$GID = $sides[1];
# 		}
# 	}
# 	$t = time();
	
# 	if($GS eq '2'){
# 		$mysql = "update games set game_state = $GS, time_game_started = $t where id = $GID";
# 	}else{
# 		$mysql = "update games set game_state = $GS where id = $GID";
# 	}
# 	&runSQL($mysql);
 }

# sub runSQL{
# 	my ($mysql) = @_;	
# 	&logData("SQL:$mysql");
# 	my $myParaQuery = $myDbC->prepare($mysql) or &myDie("SQL ERROR: on sql:$mysql". $myDbC->errstd . " Quitting");
# 	$myParaQuery->execute() or &myDie("problem running sql: $mysql");
# 	my @rows ;
# 	my @row;
# 	if($mysql =~ m/^select/i){
# 		&logData("SELECT FOUND AT START OF STRING");
# 		while(  my @row = $myParaQuery->fetchrow_array() ){
# 			push @rows, \@row;
# 		}	
# 	}
# 	return @rows;
# }


sub myDie{
	my ($inStr) = @_;
	logData($inStr);
	print "START_GAME:$inStr";
	#exit;	
}

sub logData{
	
	open (MYFILE, ">>$logFileName");
	my ($inStr) = @_;
	print MYFILE "$inStr\n";
	close (MYFILE); 	
}


