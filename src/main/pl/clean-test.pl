
print 'a[' . &clean("\n    hello    ") . ']' . "\n";
print 'b[' . &clean("    hello    \n") . ']' . "\n";
print 'c[' . &clean("    hello    \r\n") . ']' . "\n";
print 'd[' . &clean("\r\n    hello    ") . ']' . "\n";
print 'e[' . &clean("\0    hello    ") . ']' . "\n";
print 'f[' . &clean("    hello    \0") . ']' . "\n";
print 'g[' . &clean("hello ") . ']' . "\n";
print 'h[' . &clean(" hello ") . ']' . "\n";
print 'i[' . &clean("        ") . ']' . "\n";
print 'j[' . &clean("       ") . ']' . "\n";
print 'k[' . &clean("  ") . ']' . "\n";
print 'l[' . &clean("    \n  ") . ']' . "\n";
print 'm[' . &clean("    \n") . ']' . "\n";
print 'n[' . &clean("    \r  ") . ']' . "\n";
print 'o[' . &clean("    \r") . ']' . "\n";
print 'p[' . &clean("    \0\0") . ']' . "\n";
print 'q[' . &clean("\0\0    ") . ']' . "\n";

sub clean {
    my $r = shift;
    $r =~ s/^\0+//g;
    $r =~ s/\0+$//g;
    $r =~ s/^\s+//g;
    $r =~ s/\s+$//g;
    return (length($r) == 0 ? undef : $r);
}
