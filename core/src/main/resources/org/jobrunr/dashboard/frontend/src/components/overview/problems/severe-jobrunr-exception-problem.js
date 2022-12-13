import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import {Alert, AlertTitle} from '@material-ui/lab';
import {Button, Dialog, Link, Snackbar} from "@material-ui/core";
import MuiDialogTitle from "@material-ui/core/DialogTitle";
import MuiDialogContent from "@material-ui/core/DialogContent";
import Highlight from "react-highlight";

const useStyles = makeStyles(theme => ({
    alert: {
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    }
}));

const SevereJobRunrExceptionProblem = (props) => {
    const classes = useStyles();

    const [copyStatus, setCopyStatus] = React.useState(null);
    const [issueDialog, setIssueDialogContent] = React.useState(null);

    const handleCloseSnackbar = (event, reason) => setCopyStatus(null);
    const handleCloseDialog = (event, reason) => setIssueDialogContent(null);

    const dismissProblem = () => {
        fetch(`/api/problems/severe-jobrunr-exception`, {
            method: 'DELETE'
        })
            .then(resp => props.refresh())
            .catch(error => console.log(error));
    }

    const showDialogAsContentCouldNotBeCopied = () => {
        setIssueDialogContent(props.problem.githubIssueBody);
    }

    const copyToClipboard = () => {
        console.log(props.problem.githubIssueBody);
        if (navigator.clipboard) {
            navigator.clipboard.writeText(props.problem.githubIssueBody)
                .then(
                    () => setCopyStatus({
                        severity: 'success',
                        message: 'Successfully copied issue data to the clipboard'
                    }),
                    showDialogAsContentCouldNotBeCopied
                );
        } else {
            showDialogAsContentCouldNotBeCopied();
        }
    }

    return (
        <Alert className={classes.alert} severity="error" action={
            <Button color="inherit" size="small" onClick={dismissProblem}>
                DISMISS
            </Button>
        }>
            <AlertTitle><h4 className={classes.alertTitle}>Fatal</h4></AlertTitle>
            {
                <>JobRunr encountered an exception that should not happen. To resolve this issue, can you please
                    report the issue details to <a href="mailto:f@reportix.com" target="_blank" rel="noopener noreferrer">f@reportix.com</a>?
                    To make life easy, you can <Link onClick={copyToClipboard} color="initial">click here</Link> to copy
                    all necessary information to your clipboard and paste it in the Github issue. <br/></>
            }
            {copyStatus &&
                <Snackbar open={true} autoHideDuration={3000} onClose={handleCloseSnackbar}>
                    <Alert severity={copyStatus.severity}>
                        {copyStatus.message}
                    </Alert>
                </Snackbar>
            }
            {issueDialog &&
                <Dialog open={true} onClose={handleCloseDialog} aria-labelledby="customized-dialog-title" >
                    <MuiDialogTitle id="customized-dialog-title" onClose={handleCloseDialog}>
                        Could not copy issue data to clipboard
                    </MuiDialogTitle>
                    <MuiDialogContent dividers>
                        JobRunr could not copy the issue data to the clipboard (are you running an older browser or are you not using https?).
                        Please copy the data below and paste it in the Github issue as is.
                        <Highlight language='yaml'>
                            {issueDialog}
                        </Highlight>
                    </MuiDialogContent>
                </Dialog>
            }
        </Alert>
    );
}

export default SevereJobRunrExceptionProblem;