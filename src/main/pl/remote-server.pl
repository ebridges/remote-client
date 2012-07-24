#!/usr/bin/perl -w

use strict;
use IO::Socket qw(:DEFAULT :crlf);
use IO::Select;
use POSIX qw/strftime/;
use constant TERM => "\0";
$/ = "\0";

use constant PORT => 2007;
my $port = shift || PORT;

my $stopped = undef;
my $disconnectRequest = undef;
$SIG{INT} = sub { &log('shutting down.') ; $stopped = 'yes' };

my $sock = IO::Socket::INET-> new (
    Listen => 20,
    LocalPort => $port,
    Timeout => 60, # seconds
    Reuse => 1
    ) or die "Can't create listening socket on port $port: $!\n";
my $selector = new IO::Select();
$selector->add($sock);

&log("waiting for incoming connections on port %s", $port);

while( not $stopped and (my @read_handles = $selector->can_read(undef)) ) {
    for my $read_handle (@read_handles) {
        if($read_handle == $sock) {
            my $new_socket = $read_handle->accept();
            $selector->add($new_socket);
	    &log('received connection from %s', $new_socket->peerhost);
        } else {
            my $request;
            my $status = &read_request($read_handle, \$request);
            if($status > 0) {
                &log('read %d bytes from client', $status);
                if($status > 0) {
                    # normal input
                    my $response = &handle_request($request);
                    &write_response($selector, $response);
                } else {
                    &log('client closed socket.');
                }
	    } elsif($status == 0) {
		&log("read zero bytes from client");
		$disconnectRequest = 'yes';
            } else {
                &log("error reading from client: $!");
            }
	    &finish_request($selector, $read_handle)
		if $disconnectRequest;
        }
    }
}

sub finish_request {
    my $rs = shift;
    my $fh = shift;
    $rs->remove($fh);
    close($fh);
    $disconnectRequest = undef;
}

sub read_request {
    my $fh = shift;
    my $buf = shift;

    my $status = sysread($fh, $$buf, 512);
    $$buf =~ s/\s+$//g;
    &log('read value from socket: [%s]', $$buf);

    return $status;
}

sub handle_request {
    my $request = &clean(shift);
    my $response = $request;

    if(not $request) {
	&log('got a disconnect request.');
	$disconnectRequest = 'yes';
    }

    elsif($request eq '<policy-file-request/>'){
	&log('got a x-domain policy request');
	$response = '<?xml version="1.0"?><cross-domain-policy><allow-access-from domain="*" to-ports="*"/></cross-domain-policy>';
    }

    elsif($request =~ m/ping/i) {
	&log('got a ping request.');
	$response = 'PONG';
    }
	
    elsif($request eq 'READY') {
	&log('got a READY request.');
    }

    elsif($request =~ m/^echo/) {
	&log('got an echo request.');
	$response = (split /\:/, $request)[1];
    }
    
    elsif($request =~ m/^stop/) {
    	&log("got a stop request.");
    	$stopped = 'yes';
	$response = 'STP';
    }
    
    else {
	&log("got unrecognized request [%s]", $request);
	$response = 'UNK';
    }
    
    return $response;
}

sub clean {
    my $r = shift;
    $r =~ s/^\0+//g;
    $r =~ s/\0+$//g;
    $r =~ s/^\s+//g;
    $r =~ s/\s+$//g;

    return (length($r) == 0 ? undef : $r);
}

sub write_response {
    my $s = shift;
    my $r = shift;

    
    for my $write_handle (my @write_handles = $s->can_write(undef)) {
	if($r) {
	    &log("writing response [%s] back to client", $r);
	    syswrite $write_handle, $r . TERM;
	} else {
	    &log("no message to write");
	}
    }
}

sub log {
    my $now = strftime("%Y-%m-%d/%H:%M:%S", localtime);
    my $mesg = shift;
    my $m = '';
    if(not defined $mesg) {
	$m = 'no message';
    } elsif(defined @_ and scalar @_) {
    	$m = sprintf $mesg, @_;
    } else {
    	$m = $mesg;
    }
    print STDERR "[$now] $m\n";
}
