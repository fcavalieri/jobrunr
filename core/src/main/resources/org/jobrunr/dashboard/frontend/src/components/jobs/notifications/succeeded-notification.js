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

//JobRunrPlus: support disabling of succeeded jobs removal
const SucceededNotification = (props) => {
    const classes = useStyles();

    const job = props.job;
    const [serverStats, setServerStats] = React.useState(serversState.getServers());
    React.useEffect(() => {
        serversState.addListener(setServerStats);
        return () => serversState.removeListener(setServerStats);
    }, [])

    var automaticStateChangeMessage = "";
    if (!(serverStats === undefined || serverStats[0] === undefined)) {
        const deleteDuration = serverStats[0].deleteSucceededJobsAfter;
        const deleteDurationInSec = deleteDuration.toString().startsWith('PT') ? convertISO8601DurationToSeconds(deleteDuration) : deleteDuration;
        if (deleteDurationInSec > 0) {
            const succeededState = job.jobHistory[job.jobHistory.length - 1]
            const succeededDate = new Date(succeededState.createdAt);
            const deleteDate = new Date(succeededDate.getTime() + (deleteDurationInSec * 1000));
            automaticStateChangeMessage = (<span>It will automatically go to the deleted state <TimeAgo date={deleteDate} title={deleteDate.toString()}/>.</span>);
        }
    }

    return (
        <Grid item xs={12}>
            <Paper>
                <Alert severity="info" className={classes.alert}>
                    <strong>This job has succeeded.</strong> {automaticStateChangeMessage}
                </Alert>
            </Paper>
        </Grid>
    );
};

export default SucceededNotification;
