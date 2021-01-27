use std::ffi::OsString;
use std::process::{Command, Stdio};
use std::io;

pub struct ExitCode(pub Option<i32>);

pub fn run_rustc_skipping_cargo_checking(
    rustc_executable: OsString,
    args: Vec<OsString>,
) -> io::Result<ExitCode> {
    let is_cargo_check = args.iter().any(|a| {
        let a = a.to_string_lossy();
        a.starts_with("--emit=") && a.contains("metadata") && !a.contains("link")
    });
    if is_cargo_check {
        return Ok(ExitCode(Some(0)));
    }
    run_rustc(rustc_executable, args)
}

fn run_rustc(rustc_executable: OsString, args: Vec<OsString>) -> io::Result<ExitCode> {
    let mut child = Command::new(rustc_executable)
        .args(args)
        .stdin(Stdio::inherit())
        .stdout(Stdio::inherit())
        .stderr(Stdio::inherit())
        .spawn()?;
    Ok(ExitCode(child.wait()?.code()))
}
