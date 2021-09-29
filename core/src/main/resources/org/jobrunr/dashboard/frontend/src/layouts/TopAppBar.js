import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Chip from '@material-ui/core/Chip';
import Toolbar from '@material-ui/core/Toolbar';
import Button from '@material-ui/core/Button';
import {Link as RouterLink} from 'react-router-dom';
import statsState from "StatsStateContext.js";
import logo from '../assets/jobrunr-logo-white.png';

const useStyles = makeStyles(theme => ({
    root: {
        flexGrow: 1,
    },
    menuButton: {
        //paddingTop: "0px",
        marginRight: theme.spacing(2),
    },
    appBar: {
        zIndex: theme.zIndex.drawer + 1,
        background: `linear-gradient(90deg, rgba(0,0,0,1) 0%, rgba(2,0,36,1) 60%, rgb(0, 62, 126) 100%)`,
        minHeight: `48px`,
        maxHeight: `48px`
    },
    toolBar: {
        //zIndex: theme.zIndex.drawer + 1,
        //background: `linear-gradient(90deg, rgba(0,0,0,1) 0%, rgba(2,0,36,1) 60%, rgb(0, 62, 126) 100%)`,
        minHeight: `48px`,
        maxHeight: `48px`
    },
    titleLink: {
        color: "inherit",
        textTransform: "none",
        fontSize: "1.25rem",
        fontWeight: "300",
        boxSizing: "content-box",

        "&:hover": {
            color: "inherit"
        },
        "&:visited": {
            color: "#fff"
        }
    },
    logo: {
        width: 'auto',
        height: '30px',
        paddingRight: '12px'
    },
    buttons: {
        '& > *': {
            marginTop: "6px"
        },
        '& div.MuiChip-root': {
            height: 'initial',
            marginLeft: '6px',
            fontSize: '0.75rem'
        },
        '& div span.MuiChip-label': {
            padding: '0 8px'
        },
        margin: "0 50px",
        height: "48px",
        flexGrow: 1,
    },
    content: {
        flexGrow: 1,
        padding: theme.spacing(3),
        marginTop: 56
    },
    paper: {
        padding: theme.spacing(2),
        textAlign: 'center',
        color: theme.palette.text.secondary,
    },
}));

const TopAppBar = () => {
    const classes = useStyles();


    const [stats, setStats] = React.useState(statsState.getStats());
    React.useEffect(() => {
        statsState.addListener(setStats);
        return () => statsState.removeListener(setStats);
    }, [])

    return (
        <AppBar position="fixed" className={classes.appBar}>
            <Toolbar className={classes.toolBar}>
                <img className={classes.logo} src={logo} alt="JobRunr"/>
                <Button className={classes.titleLink} id="dashboard-btn" color="inherit" component={RouterLink} to="/dashboard/overview">
                    {process.env.REACT_APP_TITLE}
                </Button>
                <div className={classes.buttons}>
                    <Button id="jobs-btn" color="inherit" component={RouterLink} to="/dashboard/jobs">
                        Jobs <Chip color="secondary" label={stats.enqueued}/>
                    </Button>
                    <Button id="recurring-jobs-btn" color="inherit" component={RouterLink}
                            to="/dashboard/recurring-jobs">
                        Recurring Jobs <Chip color="secondary" label={stats.recurringJobs}/>
                    </Button>
                    <Button id="servers-btn" color="inherit" component={RouterLink} to="/dashboard/servers">
                        Servers <Chip color="secondary" label={stats.backgroundJobServers}/>
                    </Button>
                </div>
            </Toolbar>
        </AppBar>
    );
}

export default TopAppBar;