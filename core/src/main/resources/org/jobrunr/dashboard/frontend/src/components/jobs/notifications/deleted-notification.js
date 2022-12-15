import Alert from "@material-ui/lab/Alert";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";
import serversState from "../../../ServersStateContext";
import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import TimeAgo from "react-timeago/lib";


const useStyles = makeStyles(() => ({
    alert: {
        fontSize: '1rem'
    }
}));

//JobRunrPlus: support disabling of deleted jobs removal
const DeletedNotification = (props) => {
    const classes = useStyles();

    const job = props.job;
    const [serverStats, setServerStats] = React.useState(serversState.getServers());
    React.useEffect(() => {
        serversState.addListener(setServerStats);
        return () => serversState.removeListener(setServerStats);
    }, [])

    var automaticRemovalMessage = "";
    if (!(serverStats === undefined || serverStats[0] === undefined)) {
        const deleteDuration = serverStats[0].permanentlyDeleteDeletedJobsAfter;
        const deleteDurationInSec = deleteDuration.toString().startsWith('PT') ? convertISO8601DurationToSeconds(deleteDuration) : deleteDuration;
        if (deleteDurationInSec > 0) {
            const deletedState = job.jobHistory[job.jobHistory.length - 1]
            const deletedDate = new Date(deletedState.createdAt);
            const deleteDate = new Date(deletedDate.getTime() + (deleteDurationInSec * 1000));
            automaticRemovalMessage = (<span>It will automatically be removed <TimeAgo date={deleteDate} title={deleteDate.toString()}/>.</span>);
        }
    }

    return (
        <Grid item xs={12}>
            <Paper>
                <Alert severity="info" className={classes.alert}>
                    <strong>This job is deleted.</strong> {automaticRemovalMessage}
                </Alert>
            </Paper>
        </Grid>
    )
};

export default DeletedNotification;
