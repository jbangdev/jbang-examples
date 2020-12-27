///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS com.amazonaws:aws-java-sdk:1.11.905
/** Example illustrating s3 upload of files w/jbang.
 *
 * Idea from https://twitter.com/Yoakum/status/1329604887563415554
 *
 * Resources used:
 *  https://www.baeldung.com/aws-s3-java
 *  https://www.baeldung.com/aws-s3-multipart-upload
 *  https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-s3-transfermanager.html
 *  https://docs.aws.amazon.com/code-samples/latest/catalog/java-s3-src-main-java-aws-example-s3-XferMgrProgress.java.html
 */

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.amazonaws.services.s3.transfer.Upload;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.System.out;

@Command(name = "s3upload", mixinStandardHelpOptions = true, version = "s3upload 0.1",
        description = "s3upload made with jbang")
class s3upload implements Callable<Integer> {

    @CommandLine.Option(names={"--id"}, defaultValue = "${AMAZON_ID}", required = true)
    private String amazonId;

    @CommandLine.Option(names={"--key"}, defaultValue = "${AMAZON_KEY}", required = true)
    private String amazonKey;

    @CommandLine.Option(names={"--region"}, defaultValue = "${AMAZON_REGION:-eu-west-1}", required = true)
    private String amazonRegion;

    @CommandLine.Option(names={"--bucket"}, defaultValue = "${user.name}-bucket", required = true)
    private String bucket;

    @Parameters
    private List<File> files;

    public static void main(String... args) {
        int exitCode = new CommandLine(new s3upload()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...

        out.println(amazonId);
        out.println(amazonKey);
        out.println(amazonRegion);
        out.println(files);

        AWSCredentials credentials = new BasicAWSCredentials(
                amazonId,
                amazonKey
        );

        AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.fromName(amazonRegion))
                .build();

        if(s3client.doesBucketExistV2(bucket)) {
            out.println("Using existing bucket: " + bucket);
        } else {
            System.out.println("Creating bucket:" + bucket);
            s3client.createBucket(bucket);
        }

        TransferManager tm = TransferManagerBuilder.standard()
                .withS3Client(s3client)
                .withMultipartUploadThreshold((long) (5 * 1024 * 1025))
                .build();

        ProgressListener progressListener = progressEvent -> System.out.println(
                "Transferred bytes: " + progressEvent.getBytesTransferred());

        try {
            MultipleFileUpload xfer = tm.uploadFileList(bucket,
                    bucket, new File("."), files);
            // loop with Transfer.isDone()
            showMultiUploadProgress(xfer);
            // or block with Transfer.waitForCompletion()
            xfer.waitForCompletion();
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
        tm.shutdownNow();

        return 0;
    }


    // Prints progress of a multiple file upload while waiting for it to finish.
    public static void showMultiUploadProgress(MultipleFileUpload multi_upload) {
        // print the upload's human-readable description
        System.out.println(multi_upload.getDescription());

        Collection<? extends Upload> sub_xfers = new ArrayList<Upload>();
        sub_xfers = multi_upload.getSubTransfers();

        do {
            System.out.println("\nSubtransfer progress:\n");
            for (Upload u : sub_xfers) {
                System.out.println("  " + u.getDescription());
                if (u.isDone()) {
                    Transfer.TransferState xfer_state = u.getState();
                    System.out.println("  " + xfer_state);
                } else {
                    TransferProgress progress = u.getProgress();
                    double pct = progress.getPercentTransferred();
                    printProgressBar(pct);
                    System.out.println();
                }
            }

            // wait a bit before the next update.
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                return;
            }
        } while (multi_upload.isDone() == false);
        // print the final state of the transfer.
        Transfer.TransferState xfer_state = multi_upload.getState();
        System.out.println("\nMultipleFileUpload " + xfer_state);
    }

    // prints a simple text progressbar: [#####     ]
    public static void printProgressBar(double pct) {
        // if bar_size changes, then change erase_bar (in eraseProgressBar) to
        // match.
        final int bar_size = 40;
        final String empty_bar = "                                        ";
        final String filled_bar = "########################################";
        int amt_full = (int) (bar_size * (pct / 100.0));
        System.out.format("  [%s%s]", filled_bar.substring(0, amt_full),
                empty_bar.substring(0, bar_size - amt_full));
    }

}
