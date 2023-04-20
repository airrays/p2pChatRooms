import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class main {
    public static void main(String[] args) throws Exception {
        OptionalArg opa = new OptionalArg();
        int pport = 0;
        int iport = 0;
        CmdLineParser argparser = new CmdLineParser(opa);
        if(args.length >= 1){
            String [] portnumber;
            if(args.length == 2){
                portnumber = new String[2];
                portnumber[0] = args[0];
                portnumber[1] = args[1];
            }
            else{
                portnumber = new String[4];
                portnumber[0] = args[0];
                portnumber[1] = args[1];
                portnumber[2] = args[2];
                portnumber[3] = args[3];
            }
            argparser.parseArgument(portnumber);
            pport= opa.getpPort();
            iport = opa.getiPort();
        }
        else{
            String[] arg = new String[0];
            argparser.parseArgument(arg);
            pport= opa.getpPort();
            iport = opa.getiPort();
        }
        Server server = new Server(pport);
        server.start();
        Peer peer = new Peer(server,iport,pport);
        server.setPeer(peer);

        peer.startGetPeerInput();
    }

        static class OptionalArg {
        @Option(required=false, name="-p")
        private int pPort = 4444;
        @Option(required=false, name="-i")
        private int iPort = -1;

        public int getpPort(){
            return pPort;
        }
        public int getiPort() {return iPort; }

    }
}

